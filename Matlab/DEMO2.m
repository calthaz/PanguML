%Lattice Detection....


%Start Input setup....
if ismac
[filename, pathname, fNum] = uigetfile('*.tif;*.bmp;*.jpg', 'Open Image', [pwd,'/images']);
else
[filename, pathname, fNum] = uigetfile('*.tif;*.bmp;*.jpg', 'Open Image', [pwd,'\images\']);
end

if isequal(filename, 0)
    %break;??? exit?
    return;
end

inputpath=fullfile(pathname, filename);
savepath='.\results\';
fname=[filename(1:size(filename,2)-4),'_result']; % this is to save the final detected lattice....
%End Input setup....
intermediatepath=sprintf('%s%s\\',savepath,fname);
mkdir(savepath);
mkdir(intermediatepath);


tic;
%Phase I - best t1 t2 proposal......
[rgb,pts,mIdxT1T2,cMemberPT,cCellMPT,mAscore,retClusters]=PhaseI(inputpath,7);


[h,w,c]=size(rgb);
%we are now going to find the biggest pattern!
disp('mIdxT1T2 size,1');
disp(size(mIdxT1T2,1));
disp('loop');
maxIdx = size(mIdxT1T2,1);
maxArea = 0;
for i = size(mIdxT1T2,1):-1:1
   sprintf('----------------loop: %d--------------',i);
   reti=mIdxT1T2(i,1:3);
   disp('>>----reti is');
   disp(reti);
   if mIdxT1T2(i,4)>=3
        member=cMemberPT{i};
        if ~isempty(member)
            pt_sel=retClusters{i};
            %t1,t2 vector...........
            t1=[pt_sel(:,reti(2)) pt_sel(:,reti(1))];
            t2=[pt_sel(:,reti(3)) pt_sel(:,reti(1))];
            vx=[t1(1,1), t1(1,2), t2(1,1)];
            vy=[t1(2,1), t1(2,2), t2(2,1)];
            area=polyarea(vx,vy);
            disp('t1 is ');
            disp(t1);
            disp('t2 is ');
            disp(t2);

            disp('vx is ');
            disp(vx);
            disp('vy is ');
            disp(vy);

            disp('>>area is');
            disp(area)
            if area>maxArea
                maxArea=area;
                maxIdx=i;
            end
        end
   end
end
disp(maxIdx);
disp(maxArea);
%return;

reti=mIdxT1T2(maxIdx,1:3);
disp('final reti');
disp(reti)
if mIdxT1T2(maxIdx,4)>=3
    member=cMemberPT{maxIdx};
    if ~isempty(member)
        pt_sel=retClusters{maxIdx};

        handle=figure(9);

        clf;imshow(rgb);hold on;

        plot(pts(1,:),pts(2,:),'o','color','w','MarkerFaceColor','b','MarkerSize',3);
        plot(pt_sel(1,:),pt_sel(2,:),'o','color','r','MarkerFaceColor','c','MarkerSize',8);
        plot(member(1,:),member(2,:),'V','color','r','MarkerFaceColor','y','MarkerSize',5);
        %t1,t2 vector...........
        t1=[pt_sel(:,reti(2)) pt_sel(:,reti(1))];
        t2=[pt_sel(:,reti(3)) pt_sel(:,reti(1))];
        vx=[t1(1,1), t1(1,2), t2(1,1)];
        vy=[t1(2,1), t1(2,2), t2(2,1)];
        area=polyarea(vx,vy);
        disp('t1 is ');
        disp(t1);
        disp('t2 is ');
        disp(t2);

        disp('vx is ');
        disp(vx);
        disp('vy is ');
        disp(vy);

        disp('area is');
        disp(area)
       
        plot(t1(1,:),t1(2,:),'y','linewidth',4);
        plot(t2(1,:),t2(2,:),'y','linewidth',4);
        plot(pt_sel(1,:),pt_sel(2,:),'o','color','r','MarkerFaceColor','c','MarkerSize',8);
        [h,w,c]=size(rgb);
        set(handle,'Position',[100 100 w+200 h+100]);
        set(handle,'PaperUnits','points','PaperSize',[w+200 h+100]);
        title(sprintf('t1t2 proposal inlier(%d,%f) Modified Ascore %f)',mIdxT1T2(maxIdx,4),mIdxT1T2(maxIdx,4)/size(pt_sel,2),mAscore(maxIdx)));axis off;
        legend('KLT','MS clustering','t_1,t_2 member','t_1,t_2 proposal','location','EastOutside');
        text(w/2,h+30,'t_1 and t_2 Proposal','HorizontalAlignment','center');

        hold off;
        drawLatticeFromProposalCell(cCellMPT{maxIdx},'r',5);
        hold on;
        plot(t1(1,:),t1(2,:),'k','linewidth',6);
        plot(t2(1,:),t2(2,:),'k','linewidth',6);
        plot(t1(1,:),t1(2,:),'y','linewidth',2);
        plot(t2(1,:),t2(2,:),'y','linewidth',2);
        %print('-f1','-djpeg','-r300',sprintf('%sc1_c6v4%s%s%.6d_%.2d.jpg',savepath,str,prefix{mmm},fileidx,i)  );
        hold off;
        clear pt_sel;
        cTmpMPT=cCellMPT{maxIdx};
        %if possible we can adjust t1,t2 vector so that they are close to
        %orthogonal....
        [fail,cProposalMPT,tp1,tp2]=MakeShortestT(rgb,cTmpMPT,t1,t2);
        if fail==1
            cProposalMPT=cTmpMPT;
        else
            t1=tp1;
            t2=tp2;
        end
        %Phase 2 where MRF and MSBP is performed.....
        [rOriginalMPT,cRegMPT,mIsGood,mIsBoundary,boundary_width,bFail]=phase2_3rect_global_newpdf3(rgb,cProposalMPT,t1,t2,savepath,fname,2,maxIdx);
        if bFail==0
            e=toc;

            save(sprintf('%s%sresult%d.mat',savepath,fname,maxIdx),'e','rOriginalMPT','cRegMPT','mIsGood','mIsBoundary');
        end

    end
end

clear rgb;
clear mIdxT1T2;
clear cMemberPT;
clear cCellMPT;
clear retClusters;

